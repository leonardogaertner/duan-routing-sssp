import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter([]), // <--- Mudamos aqui: passamos uma lista vazia direto
    provideHttpClient()
  ]
};