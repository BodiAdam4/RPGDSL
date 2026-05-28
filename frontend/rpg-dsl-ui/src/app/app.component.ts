import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EditorState } from '@codemirror/state';
import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands';
import {
  drawSelection,
  dropCursor,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter,
  keymap,
  lineNumbers,
  rectangularSelection
} from '@codemirror/view';
import { ApiService } from './api.service';
import { DiagnosticDto, ChatHistoryMessageDto, ChatResponseDto } from './api.types';

type ChatMsg = { from: 'user' | 'system'; text: string };

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements AfterViewInit, OnDestroy {
  @ViewChild('editorHost', { static: true }) private editorHost?: ElementRef<HTMLDivElement>;

  source = `initPrompt {
  "A small dark fantasy scene. The tone is tense and quiet. The narration should feel atmospheric and uncertain."
}

items {
  item RustyKey {
    description: "An old iron key covered in rust"
    rarity: "Common"
  }

  stackable item HealingPotion {
    description: "A weak healing tonic"
    healAmount: 3
    rarity: "Common"
  }
}

actors {
  actor GateGuard {
    description: "A tired but alert guard"
    faction: "Watch"
    suspicion: 1
  }
}

vars {
  hp: 5
  suspicion: 0
  gold: 2
}

start {
  guard1 as GateGuard
  give 1 HealingPotion
}

rules {
  rule HealIfNeeded chance 100 {
    requires {
      has HealingPotion and hp is less than 6
    }
    success {
      consume 1 HealingPotion
      add HealingPotion.healAmount to hp
      narrate "You quietly drink the potion and feel a little stronger."
    }
    fail {
      narrate "You cannot heal right now."
    }
  }
}

world {
  scenes {
    scene gate {
      description: "A wooden gate blocks the road into the village."

      present {
        guard1
      }

      on enter {
        narrate "Cold wind moves through the empty road."
        guard1 says "State your business."
      }

      intents {
        intent talk {
          description: "Try to convince the guard to let you pass"
        }

        intent heal {
          description: "Drink your potion before speaking"
        }
      }

      on heal chance 100 {
        success {
          use HealIfNeeded
        }
        fail {
          narrate "Nothing happens."
        }
      }

      on talk chance 60 {
        success {
          guard1 says "Very well. You may enter."
          go to village
        }
        fail {
          guard1 says "No. Not tonight."
          add 1 to suspicion
        }
      }
    }

    scene village {
      description: "The village is silent, lit by weak lanterns."
      end scene
    }
  }
}`;

  diagnostics: DiagnosticDto[] = [];
  validating = false;
  starting = false;
  sending = false;
  started = false;

  chatInput = '';
  chat: ChatMsg[] = [];
  private editorView?: EditorView;

  constructor(private api: ApiService) {}

  ngAfterViewInit(): void {
    if (!this.editorHost?.nativeElement) {
      return;
    }

    this.editorView = new EditorView({
      parent: this.editorHost.nativeElement,
      state: EditorState.create({
        doc: this.source,
        extensions: [
          lineNumbers(),
          highlightActiveLineGutter(),
          history(),
          drawSelection(),
          dropCursor(),
          EditorState.allowMultipleSelections.of(true),
          rectangularSelection(),
          highlightActiveLine(),
          keymap.of([
            indentWithTab,
            ...defaultKeymap,
            ...historyKeymap
          ]),
          EditorView.lineWrapping,
          EditorView.editable.of(true),
          EditorView.theme({
            '&': {
              height: '100%',
              outline: 'none'
            },
            '.cm-gutters': {
              backgroundColor: '#f9fafc',
              border: 'none'
            },
            '.cm-content': {
              padding: '12px 0'
            },
            '.cm-line': {
              padding: '0 12px'
            },
            '.cm-scroller': {
              fontFamily: 'inherit'
            },
            '.cm-gutterElement': {
              padding: '0 10px 0 12px'
            }
          }),
          EditorView.updateListener.of((update) => {
            if (update.docChanged) {
              this.source = update.state.doc.toString();
            }
          })
        ]
      })
    });
  }

  ngOnDestroy(): void {
    this.editorView?.destroy();
  }

  validate(): void {
    this.validating = true;
    this.api.validate({ source: this.source }).subscribe({
      next: (res) => {
        this.diagnostics = res.diagnostics ?? [];
        this.validating = false;
      },
      error: (err) => {
        this.diagnostics = this.extractDiagnostics(err) ?? [
          { line: 0, character: 0, severity: 'ERROR', message: 'Validate failed: ' + this.extractErrorMessage(err) }
        ];
        this.validating = false;
      }
    });
  }

  start(): void {
    this.starting = true;
    this.api.start({ source: this.source }).subscribe({
      next: (res) => {
        this.diagnostics = res.diagnostics ?? [];
        this.started = !!res.text;
        this.chat = res.text ? [{ from: 'system', text: res.text }] : [];
        this.starting = false;
      },
      error: (err) => {
        const backendDiagnostics = this.extractDiagnostics(err);
        if (backendDiagnostics) {
          this.diagnostics = backendDiagnostics;
        } else {
          this.chat.push({ from: 'system', text: 'Start failed: ' + this.extractErrorMessage(err) });
        }
        this.started = false;
        this.starting = false;
      }
    });
  }

  send(): void {
    const msg = this.chatInput.trim();
    if (!msg) return;

    if (!this.started) {
      this.chat.push({ from: 'system', text: 'Nincs elindított játék. Nyomd meg a Start-ot.' });
      this.chatInput = '';
      return;
    }

    this.chat.push({ from: 'user', text: msg });
    this.chatInput = '';
    this.sending = true;

    this.api.turn({
      source: this.source,
      history: this.chat.map((item): ChatHistoryMessageDto => ({ from: item.from, text: item.text }))
    }).subscribe({
      next: (res: ChatResponseDto) => {
        this.chat.push({ from: 'system', text: res.text });
        this.sending = false;
      },
      error: (err) => {
        this.chat.push({ from: 'system', text: 'Turn failed: ' + this.extractErrorMessage(err) });
        this.sending = false;
      }
    });
  }

  severityClass(d: DiagnosticDto): string {
    return d.severity === 'ERROR' ? 'err' : d.severity === 'WARN' ? 'warn' : 'info';
  }

  private extractDiagnostics(err: unknown): DiagnosticDto[] | null {
    const diagnostics = (err as { error?: { diagnostics?: DiagnosticDto[] } })?.error?.diagnostics;
    return Array.isArray(diagnostics) ? diagnostics : null;
  }

  private extractErrorMessage(err: unknown): string {
    const httpError = err as { error?: { message?: string }; message?: string };
    return httpError?.error?.message ?? httpError?.message ?? String(err);
  }
}
