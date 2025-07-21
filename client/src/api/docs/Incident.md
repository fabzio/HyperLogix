# Incident


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **string** |  | [optional] [default to undefined]
**turn** | **string** |  | [optional] [default to undefined]
**type** | **string** |  | [optional] [default to undefined]
**truckCode** | **string** |  | [optional] [default to undefined]
**fuel** | **number** |  | [optional] [default to undefined]
**location** | [**Point**](Point.md) |  | [optional] [default to undefined]
**daysSinceIncident** | **number** |  | [optional] [default to undefined]
**status** | **string** |  | [optional] [default to undefined]
**incidentTime** | **string** |  | [optional] [default to undefined]
**expectedRecovery** | **string** |  | [optional] [default to undefined]

## Example

```typescript
import { Incident } from './api';

const instance: Incident = {
    id,
    turn,
    type,
    truckCode,
    fuel,
    location,
    daysSinceIncident,
    status,
    incidentTime,
    expectedRecovery,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
