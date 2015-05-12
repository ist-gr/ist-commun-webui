/*global define */
define(['angular',
        'angular-locale_el'
       ], function (angular) {
	'use strict';

	var module = angular.module('core', []);

  /**
   * Make a deep copy of an object or array, assuring that there is at most
   * one instance of each object or array in the resulting structure. The
   * duplicate references (which might be forming cycles) are replaced with
   * an object of the form {$ref: PATH} where the PATH is a JSONPath string
   * that locates the first occurrence.
   * So, var a = [];a[0] = a;a[0] = a;return JSON.stringify(safeCopy(a))
   * produces the string '[{"$ref":"$"}]'.
    */

  function safeCopy(object) {

    // JSONPath is used to locate the unique object. $ indicates the top level of
    // the object or array. [NUMBER] or [STRING] indicates a child member or
    // property.

    var objects = [],   // Keep a reference to each unique object or array
      paths = [];     // Keep the path to each unique object or array

    return (function derez(value, path) {

      // The derez recurses through the object, producing the deep copy.

      var i,          // The loop counter
        name,       // Property name
        nu;         // The new object or array

      // typeof null === 'object', so go on if this value is really an object but not
      // one of the weird builtin objects.

      if (typeof value === 'object' && value !== null &&
        !(value instanceof Boolean) &&
        !(value instanceof Date)    &&
        !(value instanceof Number)  &&
        !(value instanceof RegExp)  &&
        !(value instanceof String)) {

        // If the value is an object or array, look to see if we have already
        // encountered it. If so, return a $ref/path object. This is a hard way,
        // linear search that will get slower as the number of unique objects grows.

        for (i = 0; i < objects.length; i += 1) {
          if (objects[i] === value) {
            return {$ref: paths[i]};
          }
        }

        // Otherwise, accumulate the unique value and its path.

        objects.push(value);
        paths.push(path);

        // If it is an array, replicate the array.

        if (Object.prototype.toString.apply(value) === '[object Array]') {
          nu = [];
          for (i = 0; i < value.length; i += 1) {
            nu[i] = derez(value[i], path + '[' + i + ']');
          }
        } else {

        // If it is an object, replicate the object.

          nu = {};
          for (name in value) {
            if (Object.prototype.hasOwnProperty.call(value, name)) {
              nu[name] = derez(value[name],
                path + '[' + JSON.stringify(name) + ']');
            }
          }
        }

        return nu;
      }
      return value;
    }(object, '$'));
  }

  /**
   * Replaces references to ids of objects with the objects themselves in an object that
   * was reduced by
   * http://fasterxml.github.io/jackson-annotations/javadoc/2.0.0/com/fasterxml/jackson/annotation/JsonIdentityInfo.html
   * Supports only objects which have an 'id' field for Object Identifier whose type is 'string' and there is not any chance that an id value can be found in simple type fields.
   * @param data
   * @returns {*}
   */
  function retrocycle(o, options) {
//    var t = (new Date()).valueOf();
//    console.debug('retrocycle start', o, options);
    options = angular.extend({removeCircularDeps: true}, options); // TODO Move removeCircularDeps functionality into entityCrud.js initSelection
    var referencedObjs = {};
    function rez(value, path) {
      function refObj(referencedObj) {
        if (options.removeCircularDeps && path[referencedObj.id] !== undefined) {
          console.warn('Replaced circular dependency to object with "id "'+referencedObj.id);
          return referencedObj.id;
        } else {
          return referencedObj;
        }
      }
//      console.debug(JSON.stringify(path));
      var fieldValue, referencedObj;
      if (value === null) {
        return value;
      }
      if (typeof value !== 'object') {
        referencedObj = referencedObjs[value];
        return (referencedObj === undefined ? value : refObj(referencedObj));
      }
      if (Object.prototype.toString.apply(value) === '[object Array]') {
        for(var i=0; i<value.length; i++) {
          value[i] = rez(value[i], path);
        }
      } else {
        var myId;
        for(var fieldKey in value) {
          if (!value.hasOwnProperty(fieldKey) || fieldKey.substr(0, 1) === '$') {
            continue;
          }
          fieldValue = value[fieldKey];
          if (fieldValue === undefined || fieldValue === null) {
            continue;
          }
          if (fieldKey === 'id' && typeof fieldValue === 'string') {
            myId = fieldValue;
            referencedObjs[myId] = value;
//            console.debug('Pushing', myId);
            path[myId] = true;
          } else if (typeof fieldValue === 'object') {
            value[fieldKey] = rez(fieldValue, path);
          } else {
            referencedObj = referencedObjs[fieldValue];
            if (referencedObj !== undefined) {
              value[fieldKey] = refObj(referencedObj);
            }
          }
        }
        if (myId !== undefined) {
//          console.debug('Popping', myId);
          delete path[myId];
        }
      }
      return value;
    }
    var result = rez(o, {});
//    console.debug('retrocycle end', ((new Date()) - t), 'millis', result);
    return result;
  }

	module.factory('apiService', ['$window', function($window) {
		// TODO handle more patterns
		// /xxx -> /api/, xxx-yyy/zzz -> xxx/api/
		var apiRootPath = $window.location.pathname.replace(/^(?:(.*)(?:\-.+))?\/(?:.*)$/i, '$1');

    return {
      apiPathname: apiRootPath + '/api/',
      apiRootPath: apiRootPath,
      retrocycle: retrocycle,
      safeCopy: safeCopy,
      idFromSelfLink: function (entity) {
        var selfHref = entity._links.self.href;
        return selfHref.substr(selfHref.lastIndexOf('/') + 1, selfHref.length - 1);
      }
    };
  }]);

	return module;
});
