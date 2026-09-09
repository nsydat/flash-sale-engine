package com.fse.flashsale.controller;

import com.fse.flashsale.dto.CampaignInitRequest;
import com.fse.flashsale.dto.CampaignInitResponse;
import com.fse.flashsale.dto.CampaignVerificationResponse;
import com.fse.flashsale.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Administrative campaign endpoints; protect these routes in a real deployment. */
@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {
    private final CampaignService campaigns;

    public CampaignController(CampaignService campaigns) { this.campaigns = campaigns; }

    @PostMapping("/init")
    public ResponseEntity<CampaignInitResponse> initialize(@Valid @RequestBody CampaignInitRequest request) {
        return ResponseEntity.ok(campaigns.initialize(request));
    }

    @GetMapping("/{productId}/verify")
    public ResponseEntity<CampaignVerificationResponse> verify(@PathVariable Long productId) {
        return ResponseEntity.ok(campaigns.verify(productId));
    }
}
