/*global define */
define([], function() {
  'use strict';

  function repeat(aString, nTimes)
  {
    return new Array(nTimes + 1).join(aString);
  }

  function format(aString) {
    var args = Array.prototype.slice.call(arguments, 1);
    return aString.replace(/{(\d+)}/g, function(match, number) {
      return typeof args[number] !== 'undefined' ?
        args[number] :
        match;
    });
  }

  return {
    repeat: repeat,
    format: format
  };

});