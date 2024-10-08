/**
* This file has been generated by Babel. DO NOT EDIT IT DIRECTLY
* 
* Edit the source file at src/js/index.ts
**/
"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});

var _Constants = require("./Constants");

Object.keys(_Constants).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  if (key in exports && exports[key] === _Constants[key]) return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _Constants[key];
    }
  });
});

var _interfaces = require("./interfaces");

Object.keys(_interfaces).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  if (key in exports && exports[key] === _interfaces[key]) return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _interfaces[key];
    }
  });
});

var _RmxAudioPlayer = require("./RmxAudioPlayer");

Object.keys(_RmxAudioPlayer).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  if (key in exports && exports[key] === _RmxAudioPlayer[key]) return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function get() {
      return _RmxAudioPlayer[key];
    }
  });
});