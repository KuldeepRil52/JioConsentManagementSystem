import axios from "axios";

export const makeAPICall = async (url, method, body, headers) => {
  console.log("Insidee APICall.js");
  try {
    headers.method = method;
    let apiObjAxios = { url, method };
    apiObjAxios.headers = headers;

    if (method === "POST" || method === "PUT" || method === "PATCH") {
      apiObjAxios.data = body;
    }

    return new Promise((resolve, reject) => {
      axios(apiObjAxios)
        .then(async (response) => {
          resolve(response);
        })
        .catch((err) => {
          if (err.response && err.response.data) {
            reject(err.response.data);
          } else {
            reject(err);
          }
        });
    });
  } catch (error) {
    throw new Error(
      "Something went wrong",
      error.response ? error.response.data : error
    );
  }
};
