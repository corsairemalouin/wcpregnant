/**
 * Configuration Karma pour les tests Angular.
 *
 * Le poste de développement ne dispose pas de Google Chrome : on pointe
 * le launcher sur Microsoft Edge (moteur Chromium), qui accepte le mode
 * headless. `CHROME_BIN` peut aussi être défini par l'environnement.
 */
const DEFAULT_BROWSER = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';

process.env.CHROME_BIN = process.env.CHROME_BIN || DEFAULT_BROWSER;

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage')
    ],
    reporters: ['progress'],
    browsers: ['ChromiumHeadlessNoSandbox'],
    customLaunchers: {
      ChromiumHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage']
      }
    },
    singleRun: true,
    restartOnFileChange: false
  });
};
