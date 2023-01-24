Update subscription
-------------------
* **URL**

  `/subscription/update-subscription`

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