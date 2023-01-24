Read-subscription
-----------------------
Accepts an invitation

* **URL**

  `/subscription/read-subscription/:safeId`

* **Method**

  `POST`

* **Example Payload**

```json
{
  "displaySubscriptionForCbCRequest": {
    "requestCommon": {
      "regime": "CbC",
      "conversationID": "d3937a26-a4ec-4f11-bd8d-a93fc0265701",
      "receiptDate": "2020-09-15T09:38:00Z",
      "acknowledgementReference": "8493893huer3ruihuow",
      "originatingSystem": "MDTP",
      "requestParameters": [
        {
          "paramName": "param name",
          "paramValue": "param value"
        }
      ]
    },
    "requestDetail": {
      "IDType": "CbC",
      "IDNumber": "YUDD789429"
    }
  }
}
```

* **Success Response:**

    * **Code:** 200 <br />

* **Example Success Response**

```json
{
  "displaySubscriptionForCbCResponse": {
    "responseCommon": {
      "status": "OK",
      "processingDate": "2020-08-09T11:23:45Z"
    },
    "responseDetail": {
      "subscriptionID": "yu789428932",
      "tradingName": "Tools for Traders",
      "isGBUser": true,
      "primaryContact": [
        {
          "email": "Tim@toolsfortraders.com",
          "phone": "078803423883",
          "mobile": "078803423883",
          "individual": {
            "lastName": "Taylor",
            "firstName": "Tim"
          }
        }
      ],
      "secondaryContact": [
        {
          "email": "contact@toolsfortraders.com",
          "organisation": {
            "organisationName": "Tools for Traders Limited"
          }
        }
      ]
    }
  }
}
```

* **Error Response:**

    * **Code:** 400 BAD_REQUEST <br />
      **Content:** `{"errorDetail": {
      "timestamp" : "2017-02-14T12:58:44Z",
      "correlationId": "c181e730-2386-4359-8ee0-f911d6e5f3bc",
      "errorCode": "400",
      "errorMessage": "Invalid JSON document"
      "source": "journey-dct04-service-camel",
      "sourceFaultDetail":{
      "detail":[
      "<detail generated from service>"
      ]}
      }}`

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

    * **Code:** 4XX Upstream4xxResponse <br />

  OR anything else

    * **Code:** 5XX Upstream5xxResponse <br />