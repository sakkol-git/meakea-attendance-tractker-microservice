package com.hrflow.request.service;

import com.hrflow.request.dto.RequestLineDto;
import com.hrflow.request.entity.RequestLine;
import com.hrflow.request.mapper.RequestLineMapper;
import com.hrflow.request.repository.MaterialItemRepository;
import com.hrflow.request.repository.RequestLineRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Material selection is tenant-owned, while quantities are advisory until an explicit handover. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class RequestLines {
    private final RequestLineRepository lines;
    private final MaterialItemRepository materials;
    private final RequestLineMapper mapper;

    public void replace(String tenant, String request, String type, List<RequestLineDto> input) {
        if (input == null) return;
        if (!"MATERIAL".equalsIgnoreCase(type) && !input.isEmpty()) throw invalid();
        var selected = new HashSet<String>();
        for (var line : input) {
            if (line == null || line.getMaterialItemId() == null || line.getQuantity() < 1 || line.getQuantity() > 100000
                    || !selected.add(line.getMaterialItemId()) || !materials.findByTenantIdAndId(tenant, line.getMaterialItemId())
                    .map(material -> material.isActive()).orElse(false)) throw invalid();
        }
        lines.deleteByTenantIdAndRequestId(tenant, request);
        for (var line : input) {
            lines.save(RequestLine.builder().id(UUID.randomUUID().toString()).tenantId(tenant).requestId(request)
                    .materialItemId(line.getMaterialItemId()).quantity(line.getQuantity()).notes(line.getNotes()).createdAt(Instant.now()).build());
        }
    }

    public List<RequestLineDto> read(String tenant, String request) {
        return lines.findByTenantIdAndRequestId(tenant, request).stream().map(mapper::toDto).toList();
    }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Choose each active tenant material once with a positive whole quantity"); }
}
