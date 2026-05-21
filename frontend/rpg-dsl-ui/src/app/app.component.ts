import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
  sending = false;
  started = false;

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
