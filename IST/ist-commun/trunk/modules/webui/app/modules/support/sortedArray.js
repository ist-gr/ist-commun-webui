/**
 * An implementation of John von Neumann's sorted arrays in JavaScript. Implements insertion sort and binary search for fast insertion and deletion.
 */
/*global define */
define([], function() {
  'use strict';

  function SortedArray() {
    var index = 0;
    this.array = [];
    var length = arguments.length;
    while (index < length) {
      this.insert(arguments[index++]);
    }
  }

  SortedArray.prototype.insert = function (element) {
    var array = this.array;
    var index = array.length;
    array.push(element);

    while (index) {
      var i = index, j = --index;

      if (array[i] < array[j]) {
        var temp = array[i];
        array[i] = array[j];
        array[j] = temp;
      }
    }

    return this;
  };

  SortedArray.prototype.search = function (element) {
    var low = 0;
    var array = this.array;
    var high = array.length;

    while (high > low) {
      /*jshint bitwise: false*/
      var index = (high + low) / 2 >>> 0;
      var cursor = array[index];

      if (cursor < element) {
        low = index + 1;
      } else if (cursor > element) {
        high = index;
      } else {
        return index;
      }
    }

    return -1;
  };

  SortedArray.prototype.remove = function (element) {
    var index = this.search(element);
    if (index >= 0) {
      this.array.splice(index, 1);
    }
    return this;
  };

  return SortedArray;
});