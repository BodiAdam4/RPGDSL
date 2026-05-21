export type Severity = 'ERROR' | 'WARN' | 'INFO';

export interface DiagnosticDto {
  line: number;
  character: number;
  severity: Severity;
  code?: string | null;
  message: string;
}

export interface ValidateRequestDto {
  source: string;
}
export interface ValidateResponseDto {
  diagnostics: DiagnosticDto[];
}

export interface StartRequestDto {
  source: string;
}
export interface StartResponseDto {
  text?: string | null;
  diagnostics: DiagnosticDto[];
}

export type ChatFrom = 'user' | 'system';

export interface ChatHistoryMessageDto {
  from: ChatFrom;
  text: string;
}

export interface ChatRequestDto {
  source: string;
  history: ChatHistoryMessageDto[];
}
export interface ChatResponseDto {
  text: string;
}
