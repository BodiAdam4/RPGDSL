import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../environments/environment';
import {
  ValidateRequestDto, ValidateResponseDto,
  StartRequestDto, StartResponseDto,
  ChatRequestDto, ChatResponseDto
} from './api.types';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private base = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  validate(req: ValidateRequestDto): Observable<ValidateResponseDto> {
    return this.http.post<ValidateResponseDto>(`${this.base}/validate`, req);
  }

  start(req: StartRequestDto): Observable<StartResponseDto> {
    return this.http.post<StartResponseDto>(`${this.base}/start`, req);
  }

  turn(sessionId: string, req: ChatRequestDto): Observable<ChatResponseDto> {
    return this.http.post<ChatResponseDto>(`${this.base}/turn/${encodeURIComponent(sessionId)}`, req);
  }
}