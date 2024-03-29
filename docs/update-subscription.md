Update subscription
-------------------
* **URL**

  `/subscription/update-subscription`

* **Method**

  `POST`

* **Example Payload**

```json
{
  "updateSubscriptionForCbCRequest": {
    "requestCommon": {
      "regime": "CbC",
      "receiptDate": "2020-09-09T11:23:10Z",
      "acknowledgementReference": "8493893huer3ruihuow",
      "originatingSystem": "MDTP"
    },
    "requestDetail": {
      "IDType": "CbC",
      "IDNumber": "YUDD7483202",
      "tradingName": "Tims's Tools",
      "isGBUser": true,
      "primaryContact": [
        {
          "email": "tim@toolsfortrade.com",
          "phone": "08778763213789",
          "mobile": "08778763213789",
          "individual": {
            "lastName": "Taylor",
            "firstName": "Timothy",
            "middleName": "Trent"
          }
        }
      ],
      "secondaryContact": [
        {
          "email": "contact@toolsfortrade.com",
          "organisation": {
            "organisationName": "Tools for Trade"
          }
        }
      ]
    }
  }
}
```

* **Success Response:**

    * **Code:** 200 <br />

* **Example Success Response**

```json

{ "updateSubscriptionForCbCResponse": { "responseCommon": { "status":
"OK", "processingDate": "2020-09-09T11:23:12Z" }, "responseDetail": {
"subscriptionID": "YUDD7483202" } } }
```

* **Error Response:**

    * **Code:** 400 BAD_REQUEST <br />
      **Content:** `{"errorDetail": {
      "timestamp" : "2017-02-14T12:58:44Z",
      "correlationId": "c181e730-2386-4359-8ee0-f911d6e5f3bc",
      "errorCode": "400",
      "errorMessage": "Invalid message",
      "source": "JSON validation",
      "sourceFaultDetail":{
      "detail":[
      "object has missing required properties (['regime'])"
      ]}
      }}`
    * **Code:** 500 INTERNAL_SERVER_ERROR <br />
        **Content:** `{"errorDetail": {
      "timestamp": "2016-09-21T11:30:47Z",
      "correlationId": "c181e730-2386-4359-8ee0-f911d6e5f3bc",
      "errorCode": "<code as generated by service>",
      "errorMessage": "<detail as generated by service>",
      "source": "ct-api",
      "sourceFaultDetail": {
      "detail": ["<detail generated by service>"]
      }
      }
      }`
    *  **Code:** 503 ServiceUnavailable <br />
       **Content:** `{
       "errorDetail": {
       "timestamp": "2016-09-21T11:30:47Z",
       "correlationId": "",
       "errorCode": "<code as generated by service>
       "errorMessage": "<detail as generated by service>",
       "source": "ct-api",
       "sourceFaultDetail": {
       "detail": ["<detail generated by service>"]
       }
       }
       }`
  * **Code:** 400 <br />
    **Content:** `{
    "errorDetail": {
    "timestamp": "2016-10-10T13:52:16Z",
    "correlationId": "d60de98c-f499-47f5-b2d6-e80966e8d19e",
    "errorCode": "400",
    "errorMessage": "Invalid ID",
    "source": "Back End",
    "sourceFaultDetail": {
    "detail": [
    "001 - Regime missing or invalid"
    ]
    }
    }
    }`
  * **Code:** 404 NOT_FOUND <br />
    **Content:** `{
    "errorDetail": {
    "timestamp": "2016-10-10T13:52:16Z",
    "correlationId": "d60de98c-f499-47f5-b2d6-e80966e8d19e",
    "errorCode": "404",
    "errorMessage": "Record not found",
    "source": "Back End",
    "sourceFaultDetail": {
    "detail": [
    "Record not found"
    ]
    }
    }
    }`
  * **Code:** 409 <br />
    **Content:** `{
    "errorDetail": {
    "timestamp": "2016-10-10T13:52:16Z",
    "correlationId": "d60de98c-f499-47f5-b2d6-e80966e8d19e",
    "errorCode": "409",
    "errorMessage": "Duplicate submission",
    "source": "Back End",
    "sourceFaultDetail": {
    "detail": [
    "Duplicate submission"
    ]
    }
    }
    }`
  * **Code:** 503 <br />
    **Content:** `{
    "errorDetail": {
    "timestamp": "2016-10-10T13:52:16Z",
    "correlationId": "d60de98c-f499-47f5-b2d6-e80966e8d19e",
    "errorCode": "503",
    "errorMessage": "Request could not be processed",
    "source": "Back End",
    "sourceFaultDetail": {
    "detail": [
    "003 - Request could not be processed"
    ]
    }
    }
    }`