package fr.lostaria.wakeapi.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.lostaria.wakeapi.core.InstanceStatus;
import fr.lostaria.wakeapi.core.OvhApi;
import fr.lostaria.wakeapi.core.exception.OvhApiException;
import fr.lostaria.wakeapi.core.exception.OvhApiExceptionCause;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OvhApiService {

    private final OvhApi ovhApi;
    private final InstanceService instanceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ovh.serviceId}")
    private String serviceId;

    @Value("${ovh.instanceId}")
    private String instanceId;

    public OvhApiService(OvhApi ovhApi, InstanceService instanceService) {
        this.ovhApi = ovhApi;
        this.instanceService = instanceService;
    }

    public InstanceStatus getInstanceStatus() throws OvhApiException {
        String response = ovhApi.get("/cloud/project/" + serviceId + "/instance/" + instanceId);

        try {
            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText("");
            return InstanceStatus.from(status);
        } catch (Exception e) {
            throw new OvhApiException("Invalid JSON from OVH: " + e.getMessage(), OvhApiExceptionCause.API_ERROR);
        }
    }

    public void unshelveInstance() throws OvhApiException {
        ovhApi.post("/cloud/project/" + serviceId + "/instance/" + instanceId + "/unshelve", "", true);
    }

    public void shelveInstance() throws OvhApiException, IOException {
        instanceService.endProxy();
        instanceService.clearBuildServerData();
        ovhApi.post("/cloud/project/" + serviceId + "/instance/" + instanceId + "/shelve", "", true);
    }

}
