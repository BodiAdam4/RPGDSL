import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from './api.service';
import { DiagnosticDto, ChatResponseDto } from './api.types';

type ChatMsg = { from: 'user' | 'system'; text: string };

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  source = `global{
  skills{
    skill Persuasion{ maxValue: 10; description: "Meggyőzés"; };
  }
  items{
    item HealingPotion{ maxStack: 99; description: "Gyógyital"; };
  }
  vars{ var hp = 10; }
  flags{ flag witnessAngry = false; }
}

world Demo{
  scenes{
    scene intro{
      description: "Egy tanú áll előtted.";
      intents{
        intent ask{ description: "Kérdezz"; };
        intent accuse{ description: "Vádolj"; };
      }
      on ask chance 60%{
        success{ narrate "Beszélni kezd."; }
        fail{ narrate "Hallgat."; }
      }
    }
  }
}`;

  diagnostics: DiagnosticDto[] = [];
  validating = false;
  starting = false;

  sessionId: string | null = null;
  sceneId: string | null = null;
  availableIntents: string[] = [];

  chatInput = '';
  chat: ChatMsg[] = [];

  constructor(private api: ApiService) {}

  validate(): void {
    this.validating = true;
    this.api.validate({ source: this.source }).subscribe({
      next: (res) => {
        this.diagnostics = res.diagnostics ?? [];
        this.validating = false;
      },
      error: (err) => {
        this.diagnostics = [{ line: 0, col: 0, severity: 'ERROR', message: 'Validate failed: ' + (err?.message ?? err) }];
        this.validating = false;
      }
    });
  }

  start(): void {
    this.starting = true;
    this.api.start({ source: this.source }).subscribe({
      next: (res) => {
        this.sessionId = res.sessionId;
        this.sceneId = res.sceneId;
        this.availableIntents = res.availableIntents ?? [];
        this.chat = [{ from: 'system', text: res.text }];
        this.starting = false;
      },
      error: (err) => {
        this.chat.push({ from: 'system', text: 'Start failed: ' + (err?.message ?? err) });
        this.starting = false;
      }
    });
  }

  send(): void {
    const msg = this.chatInput.trim();
    if (!msg) return;

    if (!this.sessionId) {
      this.chat.push({ from: 'system', text: 'Nincs aktív session. Nyomd meg a Start-ot.' });
      this.chatInput = '';
      return;
    }

    this.chat.push({ from: 'user', text: msg });
    this.chatInput = '';

    this.api.turn(this.sessionId, { message: msg }).subscribe({
      next: (res: ChatResponseDto) => {
        // opcionális: intent debug
        if (res.intent) {
          this.chat.push({ from: 'system', text: `(intent: ${res.intent.name}, conf: ${res.intent.confidence.toFixed(2)})` });
        }
        this.chat.push({ from: 'system', text: res.text });
        this.sceneId = res.sceneId;
        this.availableIntents = res.availableIntents ?? [];
      },
      error: (err) => {
        this.chat.push({ from: 'system', text: 'Turn failed: ' + (err?.message ?? err) });
      }
    });
  }

  severityClass(d: DiagnosticDto): string {
    return d.severity === 'ERROR' ? 'err' : d.severity === 'WARN' ? 'warn' : 'info';
  }
}