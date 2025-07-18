# StationControllerApi

All URIs are relative to *http://localhost:8080/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**createStation**](#createstation) | **POST** /stations | |
|[**deleteStation**](#deletestation) | **DELETE** /stations/{stationId} | |
|[**getStationById**](#getstationbyid) | **GET** /stations/{stationId} | |
|[**list1**](#list1) | **GET** /stations | |
|[**updateStation**](#updatestation) | **PUT** /stations/{stationId} | |

# **createStation**
> Station createStation(station)


### Example

```typescript
import {
    StationControllerApi,
    Configuration,
    Station
} from './api';

const configuration = new Configuration();
const apiInstance = new StationControllerApi(configuration);

let station: Station; //

const { status, data } = await apiInstance.createStation(
    station
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **station** | **Station**|  | |


### Return type

**Station**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**201** | Created |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **deleteStation**
> deleteStation()


### Example

```typescript
import {
    StationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new StationControllerApi(configuration);

let stationId: string; // (default to undefined)

const { status, data } = await apiInstance.deleteStation(
    stationId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **stationId** | [**string**] |  | defaults to undefined|


### Return type

void (empty response body)

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**204** | No Content |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getStationById**
> Station getStationById()


### Example

```typescript
import {
    StationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new StationControllerApi(configuration);

let stationId: string; // (default to undefined)

const { status, data } = await apiInstance.getStationById(
    stationId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **stationId** | [**string**] |  | defaults to undefined|


### Return type

**Station**

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

# **list1**
> PageStation list1()


### Example

```typescript
import {
    StationControllerApi,
    Configuration,
    Pageable
} from './api';

const configuration = new Configuration();
const apiInstance = new StationControllerApi(configuration);

let pageable: Pageable; // (default to undefined)

const { status, data } = await apiInstance.list1(
    pageable
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **pageable** | **Pageable** |  | defaults to undefined|


### Return type

**PageStation**

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

# **updateStation**
> Station updateStation(station)


### Example

```typescript
import {
    StationControllerApi,
    Configuration,
    Station
} from './api';

const configuration = new Configuration();
const apiInstance = new StationControllerApi(configuration);

let stationId: string; // (default to undefined)
let station: Station; //

const { status, data } = await apiInstance.updateStation(
    stationId,
    station
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **station** | **Station**|  | |
| **stationId** | [**string**] |  | defaults to undefined|


### Return type

**Station**

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

