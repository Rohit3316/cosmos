# COSMOS OFFBOARD Kafka Interface

This file provides a structured YAML configuration for the COSMOS-OFFBOARD Kafka interface, which defines the message structure for various updates shared by COSMOS-OFFBOARD with downstream services to support integration and monitoring.

## Steps to Add or Modify JSON for Any Changes and View in the Portal

### Modify or Add JSON in YAML
1. Open the YAML file in a text/code editor.
2. Find the channels section.
3. Edit or add a new channel with `address > message > headers` & `address > message > payload`.
4. Update fields like `properties`, `required`, or `enum` as needed.
5. Save the file.

### View in AsyncAPI Studio
1. Go to [AsyncAPI Studio](https://studio.asyncapi.com).
2. Click **Open from file** or **Paste YAML**.
3. Upload or paste your YAML to preview the spec.

### Sample Yml Doc Link
https://studio.asyncapi.com/?share=915446d4-2196-456b-bf81-9732c50d06fe

## Key Parameters in the AsyncAPI YAML File

- **channel**: Represents a communication path (e.g., topic or queue) in the messaging system.  
  Example: `rollout.status` is a channel for rollout status updates.
- **$ref**: A reference to another part of the document to reuse definitions.  
  Example: `$ref: '#/components/schemas/rollout_status'` points to the `rollout_status` schema.
- **message**: Defines the structure and metadata of the message sent or received on a channel.  
  Example: `rollout_status.message` contains details like `name`, `contentType`, and `payload`.
- **components**: Contains reusable schemas, messages, or other objects.  
  Example: `schemas` define the structure of data objects like `rollout_status`.
- **servers**: Defines the servers where the messaging system is hosted.  
  Example: `host: kafka.vehicle-rollout.org:9092` specifies the Kafka server address.
- **operations**: Defines actions (e.g., send or receive) for a specific channel.  
  Example: `action: send` specifies sending messages to a channel.

# Kafka Configuration

Below is the Kafka YAML configuration:

```yaml
asyncapi: 3.0.0
info:
  title: COSMOS OFFBOARD Kafka interface
  version: 1.0.0
  description: This file provides a structured YAML configuration for the COSMOS-OFFBOARD Kafka interface, which defines the message structure for various updates shared by COSMOS-OFFBOARD with downstream services to support integration and monitoring.
defaultContentType: application/json
servers:
  INT:
    host: kafka.vehicle-rollout.org:9092
    protocol: kafka-secure
    description: Kafka int broker
channels:
  rollout.status:
    address: rollout.status
    messages:
      rollout_status.message:
        name: rollout.status
        title: Rollout Status
        summary: Message for rollout.status
        contentType: application/json
        headers:
          $ref: '#/components/schemas/rollout_status/headers'
        payload:
          $ref: '#/components/schemas/rollout_status/payload'
  rollout.vehicle.status:
    address: rollout.vehicle.status
    messages:
      rollout_vehicle_status.message:
        name: rollout.vehicle.status
        title: Rollout Vehicle Status
        summary: Message for rollout.vehicle.status
        contentType: application/json
        headers:
          $ref: '#/components/schemas/rollout_vehicle_status/headers'
        payload:
          $ref: '#/components/schemas/rollout_vehicle_status/payload'
  vehicle.inventory:
    address: vehicle.inventory
    messages:
      vehicle_inventory.message:
        name: vehicle.inventory
        title: Vehicle Inventory
        summary: Message for vehicle.inventory
        contentType: application/json
        headers:
          $ref: '#/components/schemas/vehicle_inventory/headers'
        payload:
          $ref: '#/components/schemas/vehicle_inventory/payload'
  package.status:
    address: package.status
    messages:
      package_status.message:
        name: package.status
        title: Package Status
        summary: Message for package.status
        contentType: application/json
        headers:
          $ref: '#/components/schemas/package_status/headers'
        payload:
          $ref: '#/components/schemas/package_status/payload'
  vehicle.general:
    address: vehicle.general
    messages:
      vehicle_general.message:
        name: vehicle.general
        title: Vehicle General Status
        summary: Message for vehicle.general
        contentType: application/json
        headers:
          $ref: '#/components/schemas/vehicle_general/headers'
        payload:
          $ref: '#/components/schemas/vehicle_general/payload'
components:
  schemas:
    rollout_status:
      headers:
        type: object
        required:
          - tenant
          - rolloutName
        properties:
          tenant:
            type: string
            description: Tenant identifier
          rolloutName:
            type: string
            description: Name of the rollout
      payload:
        type: object
        required:
          - type
          - status
          - timestamp
        properties:
          type:
            type: string
            enum:
              - INFO
              - ERROR
            description: Type of message
          status:
            type: string
            enum:
              - DRAFT
              - READY
              - RUNNING
              - PAUSED
              - CANCELLED
              - FINISHED
              - FREEZING
              - UNFREEZING
              - STARTING
              - PAUSING
              - RESUMING
              - CANCELLING
              - FINISHING
              - RETRYING
              - DELETING
            description: Current status of the rollout
          errorCode:
            type: array
            items:
              type: string
            description: List of error codes
          errorMessages:
            type: array
            items:
              type: string
            description: List of error messages
          timestamp:
            type: integer
            description: Epoch time in seconds
    rollout_vehicle_status:
      headers:
        type: object
        required:
          - tenant
          - rolloutName
          - vin
          - otaMasterSerialNumber
        properties:
          tenant:
            type: string
            description: Tenant identifier
          rolloutName:
            type: string
            description: Name of the rollout
          vin:
            type: string
            description: Vehicle Identification Number
          otaMasterSerialNumber:
            type: string
            description: OTA Master Serial Number
      payload:
        type: object
        required:
          - type
          - status
          - timestamp
        properties:
          type:
            type: string
            enum:
              - INFO
              - ERROR
            description: Type of message
          status:
            type: string
            enum:
              - RUNNING
              - PAUSED
              - DD_SENT
              - CANCELLED
              - STARTING
              - PAUSING
              - RESUMING
              - CANCELLING
              - FINISHING
              - RETRYING
              - DD_ACCEPTED
              - DOWNLOAD_STARTED
              - DOWNLOAD_IN_PROGRESS
              - DOWNLOAD_COMPLETED
              - USER_SCHEDULED
              - USER_ACCEPTED
              - USER_IGNORED
              - FINISHED_SUCCESS
              - FINISHED_FAILURE
              - FINISHED_NOT_EXECUTED
              - PENDING_LOGS
              - LOG_UPLOAD_IN_PROGRESS
              - LOG_UPLOAD_SUCCESS
            description: Current status of the rollout
          messages:
            type: array
            items:
              type: string
            description: Informational messages
          errorCode:
            type: array
            items:
              type: string
            description: List of error codes
          errorMessages:
            type: array
            items:
              type: string
            description: List of error messages
          timestamp:
            type: integer
            description: Epoch time in seconds
    vehicle_inventory:
      headers:
        type: object
        required:
          - tenant
          - vin
          - otaMasterSerialNumber
        properties:
          tenant:
            type: string
            description: Tenant identifier
          vin:
            type: string
            description: Vehicle Identification Number
          otaMasterSerialNumber:
            type: string
            description: OTA Master Serial Number
      payload:
        type: object
        required:
          - inventoryDetails
          - inventorySignature
        properties:
          inventoryDetails:
            type: string
            description: Base64 encoded inventory details
          inventorySignature:
            type: object
            required:
              - signature
              - signatureType
            properties:
              signature:
                type: string
                description: Signature string
              signatureType:
                type: string
                enum:
                  - SHA256withECC
                description: Type of signature
    package_status:
      headers:
        type: object
        required:
          - tenant
          - fileType
        properties:
          tenant:
            type: string
            description: Tenant identifier
          fileType:
            type: string
            enum:
              - ARTIFACT
              - ESP
              - RSP
            description: Type of file
          vin:
            type: string
            description: Vehicle Identification Number
          otaMasterSerialNumber:
            type: string
            description: OTA Master Serial Number
      payload:
        type: object
        required:
          - type
          - fileId
          - fileName
          - status
          - timeStamp
        properties:
          type:
            type: string
            enum:
              - INFO
              - ERROR
            description: Type of message
          fileId:
            type: integer
            description: Unique file identifier
          fileName:
            type: string
            description: Name of the file
          SHA256:
            type: string
            description: SHA256 hash of the file
          status:
            type: string
            enum:
              - STORAGE_UPLOAD_ERROR
              - CDN_UPLOAD_ERROR
              - STORAGE_UPLOAD_SUCESSSFUL
              - CDN_UPLOAD_SUCCESSFUL
              - STORAGE_DELETE_SUCCESSFUL
              - CDN_DELETE_SUCCESSFUL
              - EXPIRED
              - REPLACED
              - PURGED
            description: Status of the package
          errorCode:
            type: array
            items:
              type: string
            description: List of error codes
          errorMessages:
            type: array
            items:
              type: string
            description: List of error messages
          vehicleId:
            type: string
            description: Vehicle identifier
          timeStamp:
            type: integer
            description: Epoch time in seconds
    vehicle_general:
      headers:
        type: object
        required:
          - tenant
          - vin
          - otaMasterSerialNumber
        properties:
          tenant:
            type: string
            description: Tenant identifier
          vin:
            type: string
            description: Vehicle Identification Number
          otaMasterSerialNumber:
            type: string
            description: OTA Master Serial Number
      payload:
        type: object
        required:
          - type
          - status
          - timestamp
        properties:
          type:
            type: string
            enum:
              - INFO
              - ERROR
            description: Type of message
          status:
            type: string
            enum:
              - IDLE
              - ERC
            description: General status of the vehicle
          messages:
            type: array
            items:
              type: string
            description: Informational messages
          errorCode:
            type: array
            items:
              type: string
            description: List of error codes
          errorMessages:
            type: array
            items:
              type: string
            description: List of error messages
          timestamp:
            type: integer
            description: Epoch time in seconds
operations:
  rollout_status:
    action: send
    channel:
      $ref: '#/channels/rollout.status'
    messages:
    - $ref: '#/channels/rollout.status/messages/rollout_status.message'
  rollout_vehicle_status:
    action: send
    channel:
      $ref: '#/channels/rollout.vehicle.status'
    messages:
      - $ref: '#/channels/rollout.vehicle.status/messages/rollout_vehicle_status.message'
  vehicle_inventory:
    action: send
    channel:
      $ref: '#/channels/vehicle.inventory'
    messages:
      - $ref: '#/channels/vehicle.inventory/messages/vehicle_inventory.message'
  package_status:
    action: send
    channel:
      $ref: '#/channels/package.status'
    messages:
      - $ref: '#/channels/package.status/messages/package_status.message'
  vehicle_general:
    action: send
    channel:
      $ref: '#/channels/vehicle.general'
    messages:
      - $ref: '#/channels/vehicle.general/messages/vehicle_general.message'
