# MainControllerApi

All URIs are relative to *http://localhost:8080/api/v1*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**sayHello**](#sayhello) | **GET** / | |

# **sayHello**
> string sayHello()


### Example

```typescript
import {
    MainControllerApi,
    Configuration
} from './api';

const configuration = new Configuration();
const apiInstance = new MainControllerApi(configuration);

const { status, data } = await apiInstance.sayHello();
```

### Parameters
This endpoint does not have any parameters.


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

