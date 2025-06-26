# TruckControllerApi

All URIs are relative to *http://localhost:8080/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getTruckById**](#gettruckbyid) | **GET** /trucks/truck/{truckId} | |
|[**list**](#list) | **GET** /trucks | |

# **getTruckById**
> Truck getTruckById()


### Example

```typescript
import {
    TruckControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new TruckControllerApi(configuration);

let truckId: string; // (default to undefined)

const { status, data } = await apiInstance.getTruckById(
    truckId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **truckId** | [**string**] |  | defaults to undefined|


### Return type

**Truck**

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

# **list**
> PageTruck list()


### Example

```typescript
import {
    TruckControllerApi,
    Configuration,
    Pageable
} from './api';

const configuration = new Configuration();
const apiInstance = new TruckControllerApi(configuration);

let pageable: Pageable; // (default to undefined)

const { status, data } = await apiInstance.list(
    pageable
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **pageable** | **Pageable** |  | defaults to undefined|


### Return type

**PageTruck**

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

