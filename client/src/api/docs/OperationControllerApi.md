# OperationControllerApi

All URIs are relative to *http://localhost:8080/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**accelerateSimulation**](#acceleratesimulation) | **POST** /operation/simulation/accelerate | |
|[**cancelOrder**](#cancelorder) | **DELETE** /operation/orders/{orderId} | |
|[**decelerateSimulation**](#deceleratesimulation) | **POST** /operation/simulation/decelerate | |
|[**getOperationStatus**](#getoperationstatus) | **GET** /operation/status | |
|[**manualReplanification**](#manualreplanification) | **POST** /operation/replan | |
|[**pauseSimulation**](#pausesimulation) | **POST** /operation/simulation/pause | |
|[**registerOrder**](#registerorder) | **POST** /operation/orders | |
|[**reportIncident**](#reportincident) | **POST** /operation/trucks/{truckCode}/incident | |
|[**reportTruckBreakdown**](#reporttruckbreakdown) | **POST** /operation/trucks/{truckId}/breakdown | |
|[**reportTruckMaintenance**](#reporttruckmaintenance) | **POST** /operation/trucks/{truckId}/maintenance | |
|[**restoreTruckToIdle**](#restoretrucktoidle) | **POST** /operation/trucks/{truckId}/restore | |
|[**resumeSimulation**](#resumesimulation) | **POST** /operation/simulation/resume | |
|[**sendSimulationCommand**](#sendsimulationcommand) | **POST** /operation/simulation/command | |

# **accelerateSimulation**
> { [key: string]: string; } accelerateSimulation()


### Example

```typescript
import {
    OperationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

const { status, data } = await apiInstance.accelerateSimulation();
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

# **cancelOrder**
> { [key: string]: string; } cancelOrder()


### Example

```typescript
import {
    OperationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

let orderId: string; // (default to undefined)

const { status, data } = await apiInstance.cancelOrder(
    orderId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **orderId** | [**string**] |  | defaults to undefined|


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

# **decelerateSimulation**
> { [key: string]: string; } decelerateSimulation()


### Example

```typescript
import {
    OperationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

const { status, data } = await apiInstance.decelerateSimulation();
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

# **pauseSimulation**
> { [key: string]: string; } pauseSimulation()


### Example

```typescript
import {
    OperationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

const { status, data } = await apiInstance.pauseSimulation();
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

# **reportIncident**
> reportIncident(reportIncidentRequest)


### Example

```typescript
import {
    OperationControllerApi,
    Configuration,
    ReportIncidentRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

let truckCode: string; // (default to undefined)
let reportIncidentRequest: ReportIncidentRequest; //

const { status, data } = await apiInstance.reportIncident(
    truckCode,
    reportIncidentRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **reportIncidentRequest** | **ReportIncidentRequest**|  | |
| **truckCode** | [**string**] |  | defaults to undefined|


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

# **reportTruckMaintenance**
> reportTruckMaintenance(truckBreakdownRequest)


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

const { status, data } = await apiInstance.reportTruckMaintenance(
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

# **restoreTruckToIdle**
> restoreTruckToIdle()


### Example

```typescript
import {
    OperationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

let truckId: string; // (default to undefined)

const { status, data } = await apiInstance.restoreTruckToIdle(
    truckId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **truckId** | [**string**] |  | defaults to undefined|


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
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **resumeSimulation**
> { [key: string]: string; } resumeSimulation()


### Example

```typescript
import {
    OperationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

const { status, data } = await apiInstance.resumeSimulation();
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

# **sendSimulationCommand**
> { [key: string]: string; } sendSimulationCommand(requestBody)


### Example

```typescript
import {
    OperationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new OperationControllerApi(configuration);

let requestBody: { [key: string]: string; }; //

const { status, data } = await apiInstance.sendSimulationCommand(
    requestBody
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **requestBody** | **{ [key: string]: string; }**|  | |


### Return type

**{ [key: string]: string; }**

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

