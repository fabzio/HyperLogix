# SimulationControllerApi

All URIs are relative to *http://localhost:8080/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getSimulationStatus**](#getsimulationstatus) | **GET** /simulation/status/{simulationId} | |
|[**sendCommand**](#sendcommand) | **POST** /simulation/command/{simulationId} | |
|[**startSimulation**](#startsimulation) | **POST** /simulation/start/{simulationId} | |
|[**stopSimulation**](#stopsimulation) | **POST** /simulation/stop/{simulationId} | |

# **getSimulationStatus**
> SimulationStatus getSimulationStatus()


### Example

```typescript
import {
    SimulationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SimulationControllerApi(configuration);

let simulationId: string; // (default to undefined)

const { status, data } = await apiInstance.getSimulationStatus(
    simulationId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **simulationId** | [**string**] |  | defaults to undefined|


### Return type

**SimulationStatus**

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

# **sendCommand**
> sendCommand(simulationCommandRequest)


### Example

```typescript
import {
    SimulationControllerApi,
    Configuration,
    SimulationCommandRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new SimulationControllerApi(configuration);

let simulationId: string; // (default to undefined)
let simulationCommandRequest: SimulationCommandRequest; //

const { status, data } = await apiInstance.sendCommand(
    simulationId,
    simulationCommandRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **simulationCommandRequest** | **SimulationCommandRequest**|  | |
| **simulationId** | [**string**] |  | defaults to undefined|


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

# **startSimulation**
> startSimulation(startSimulationRequest)


### Example

```typescript
import {
    SimulationControllerApi,
    Configuration,
    StartSimulationRequest
} from './api';

const configuration = new Configuration();
const apiInstance = new SimulationControllerApi(configuration);

let simulationId: string; // (default to undefined)
let startSimulationRequest: StartSimulationRequest; //

const { status, data } = await apiInstance.startSimulation(
    simulationId,
    startSimulationRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **startSimulationRequest** | **StartSimulationRequest**|  | |
| **simulationId** | [**string**] |  | defaults to undefined|


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

# **stopSimulation**
> stopSimulation()


### Example

```typescript
import {
    SimulationControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new SimulationControllerApi(configuration);

let simulationId: string; // (default to undefined)

const { status, data } = await apiInstance.stopSimulation(
    simulationId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **simulationId** | [**string**] |  | defaults to undefined|


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

