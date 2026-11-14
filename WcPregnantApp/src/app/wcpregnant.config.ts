import { ApplicationConfig } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';

/**
 * Configuration de l'application : le client HTTP est nécessaire pour
 * appeler l'API WC Pregnant via le proxy Angular.
 */
export const appConfig: ApplicationConfig = {
  providers: [provideHttpClient()]
};
