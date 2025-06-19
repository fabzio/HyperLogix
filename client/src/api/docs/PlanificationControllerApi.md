# PlanificationControllerApi

All URIs are relative to *http://localhost:8080/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getPlanificationStatus**](#getplanificationstatus) | **GET** /planification/status/{sessionId} | |

# **getPlanificationStatus**
> PlanificationStatus getPlanificationStatus()


### Example

```typescript
import {
    PlanificationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new PlanificationControllerApi(configuration);

let sessionId: string; // (default to undefined)

const { status, data } = await apiInstance.getPlanificationStatus(
    sessionId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **sessionId** | [**string**] |  | defaults to undefined|


### Return type

**PlanificationStatus**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

