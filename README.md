
# Country by Country Reporting

Backend service to support file uploads from registered users (agent, client or organisation) on behalf of an organisation.

This service integrates with HOD, i.e. DES/ETMP.

The frontend to this service can be found [here]("https://github.com/hmrc/country-by-country-reporting-frontend/")

---

### Running the service

**Service manager:** CBCR_NEW_ALL

**Individually via service manager:** COUNTRY_BY_COUNTRY_REPORTING

**Port:** 10024

---

### API

| Task                              | Supported methods | Description                                                                                                |
|-----------------------------------|-------------------|------------------------------------------------------------------------------------------------------------|
| /subscription/read-subscription   | POST              | Allows a registered user to read and view their CbC subscipriton info [More...](docs/read-subscription.md) |
| /subscription/update-subscription | POST              | Allows a registered user to update their CbC subscipriton info [More...](docs/update-subscription.md)      |
| /validate-submission              | POST              | Validates a file upload                                                                                    |
| /validation-result                | POST              | The result of file validation                                                                              |
| /callback                         | POST              | Provides an endpoint for Upscan to reach after file upload                                                 |
| /upscan/details:uploadId          | GET               | Uses the upload ID to find the details of the upload                                                       |
| /upscan/status:uploadId           | GET               | Uses the upload ID to find the status of the upload                                                        |
| /upscan/upload                    | POST              | Requests an upload using Upscan                                                                            |
| /files/:conversationId/details    | GET               | Gets conversation details using ID from payload                                                            |
| /files/details                    | GET               | Gets conversation details                                                                                  |
| /files/:conversationId/status     | GET               | Gets status of file using conversation ID                                                                  |
| /submit                           | POST              | Submits file                                                                                               |

---

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").