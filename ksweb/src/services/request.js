// sample can see https://github.com/github/fetch
// sample ajax can see https://github.com/ded/reqwest

import 'whatwg-fetch';
import { notification } from 'antd';


function parseJSON(response) {
  return response.json();
  // return JSON.parse(response);
}

function checkStatus(response) {
  if (response.status >= 200 && response.status < 300) {
    return response;
  } else {
    var error = new Error(response.statusText)
    error.response = response
    throw error
  }
}

/**
 * Requests a URL, returning a promise.
 *
 * @param  {string} url       The URL we want to request
 * @param  {object} [options] The options we want to pass to "fetch"
 * @param  {string} errorMsg  default error msg
 * @return {object}           An object containing either "data" or "err"
 */
export function request(url, options, errorMsg) {
  return fetch(url, options)
    .then(checkStatus)
    .then(parseJSON)
    .then(function (data) {
      return data
    }).catch(function (err) {
      notification['error']({
        message: errorMsg,
        description: err.toString(),
        duration: 1.5
      });
      return err;
    });
}
