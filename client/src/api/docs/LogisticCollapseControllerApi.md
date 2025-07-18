# LogisticCollapseControllerApi

All URIs are relative to *http://localhost:8080/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getCollapseTypes**](#getcollapsetypes) | **GET** /api/collapse/types | |
|[**reportCollapse**](#reportcollapse) | **POST** /api/collapse/report | |

# **getCollapseTypes**
> Array<string> getCollapseTypes()


### Example

```typescript
import {
    LogisticCollapseControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new LogisticCollapseControllerApi(configuration);

const { status, data } = await apiInstance.getCollapseTypes();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**Array<string>**

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

# **reportCollapse**
> string reportCollapse()


### Example

```typescript
import {
    LogisticCollapseControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new LogisticCollapseControllerApi(configuration);

let sessionId: string; // (default to undefined)
let collapseType: string; // (default to undefined)
let description: string; // (default to undefined)
let severityLevel: number; // (default to undefined)
let affectedArea: string; // (default to undefined)

const { status, data } = await apiInstance.reportCollapse(
    sessionId,
    collapseType,
    description,
    severityLevel,
    affectedArea
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **sessionId** | [**string**] |  | defaults to undefined|
| **collapseType** | [**string**] |  | defaults to undefined|
| **description** | [**string**] |  | defaults to undefined|
| **severityLevel** | [**number**] |  | defaults to undefined|
| **affectedArea** | [**string**] |  | defaults to undefined|


### Return type

**string**

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

