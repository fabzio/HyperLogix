# OperationControllerApi

All URIs are relative to *http://localhost:8080/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getOperationStatus**](#getoperationstatus) | **GET** /operation/status | |
|[**manualReplanification**](#manualreplanification) | **POST** /operation/replan | |
|[**registerOrder**](#registerorder) | **POST** /operation/orders | |
|[**reportTruckBreakdown**](#reporttruckbreakdown) | **POST** /operation/trucks/{truckId}/breakdown | |

# **getOperationStatus**
> { [key: string]: any; } getOperationStatus()


### Example

```typescript
import {
    OperationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

const { status, data } = await apiInstance.getOperationStatus();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**{ [key: string]: any; }**

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

# **manualReplanification**
> { [key: string]: string; } manualReplanification()


### Example

```typescript
import {
    OperationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

const { status, data } = await apiInstance.manualReplanification();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**{ [key: string]: string; }**

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

# **registerOrder**
> registerOrder(registerOrderRequest)


### Example

```typescript
import {
    OperationControllerApi,
    Configuration,
    RegisterOrderRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

let registerOrderRequest: RegisterOrderRequest; //

const { status, data } = await apiInstance.registerOrder(
    registerOrderRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **registerOrderRequest** | **RegisterOrderRequest**|  | |


### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **reportTruckBreakdown**
> reportTruckBreakdown(truckBreakdownRequest)


### Example

```typescript
import {
    OperationControllerApi,
    Configuration,
    TruckBreakdownRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

let truckId: string; // (default to undefined)
let truckBreakdownRequest: TruckBreakdownRequest; //

const { status, data } = await apiInstance.reportTruckBreakdown(
    truckId,
    truckBreakdownRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **truckBreakdownRequest** | **TruckBreakdownRequest**|  | |
| **truckId** | [**string**] |  | defaults to undefined|


### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

