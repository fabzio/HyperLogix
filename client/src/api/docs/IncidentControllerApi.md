# IncidentControllerApi

All URIs are relative to *http://localhost:8080/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**registerIncident**](#registerincident) | **POST** /incidents | |

# **registerIncident**
> Incident registerIncident(incident)


### Example

```typescript
import {
    IncidentControllerApi,
    Configuration,
    Incident
} from './api';

const configuration = new Configuration();
const apiInstance = new IncidentControllerApi(configuration);

let incident: Incident; //

const { status, data } = await apiInstance.registerIncident(
    incident
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **incident** | **Incident**|  | |


### Return type

**Incident**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

