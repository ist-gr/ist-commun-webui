/*global define */
/**
 * In a client application:
 * 1. `copy /path/to/ist-commun-webui/modules/appConstantsTemplate.js /path/to/client/app/modules/appConstants.js`
 * 2. Edit /path/to/client/app/modules/appConstants.js
 * 3. Put the following line in the 'paths' section of /path/to/client/app/modules/requirejs-config.js
 *  'appConstants': './appConstants',
 */
define([], function () {
  'use strict';

  return {
    appName: '!change it!',
    copyright: 'Σχεδιασμός & Ανάπτυξη 2015- IST SA'
  };
});
