export type Severity = 'ERROR' | 'WARN' | 'INFO';

export interface DiagnosticDto {
  line: number;
  col: number;
  severity: Severity;
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
  sessionId: string;
  sceneId: string;
  text: string;
  availableIntents: string[];
}

export interface ChatRequestDto {
  message: string;
}
export interface ChatResponseDto {
  intent?: { name: string; confidence: number };
  text: string;
  sceneId: string;
  availableIntents: string[];
  stateDelta?: {
    vars?: Record<string, number>;
    flags?: Record<string, boolean>;
    inventory?: Record<string, number>;
    skills?: Record<string, number>;
  };
}