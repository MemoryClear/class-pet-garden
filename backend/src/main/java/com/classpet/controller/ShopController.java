package com.classpet.controller;

import com.classpet.dto.ShopDto.*;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedTeacher;
import com.classpet.service.ShopService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/shop")
public class ShopController {

    private final ShopService shopService;

    public ShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    // 获取商品列表
    @GetMapping("/items")
    public ResponseEntity<List<ShopItemResponse>> getItems(
            @AuthenticationPrincipal AuthenticatedTeacher principal) {
        return ResponseEntity.ok(shopService.getItems(principal.teacherId()));
    }

    // 添加商品
    @PostMapping("/items")
    public ResponseEntity<ShopItemResponse> addItem(
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @Valid @RequestBody ShopItemRequest req) {
        return ResponseEntity.ok(shopService.addItem(req, principal.teacherId()));
    }

    // 修改商品
    @PutMapping("/items/{id}")
    public ResponseEntity<ShopItemResponse> updateItem(
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @PathVariable String id, 
            @Valid @RequestBody ShopItemRequest req) {
        return ResponseEntity.ok(shopService.updateItem(id, req, principal.teacherId()));
    }

    // 删除商品
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> deleteItem(
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @PathVariable String id) {
        shopService.deleteItem(id, principal.teacherId());
        return ResponseEntity.ok().build();
    }

    // 兑换商品
    @PostMapping("/exchange")
    public ResponseEntity<ExchangeRecordResponse> exchange(
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @Valid @RequestBody ExchangeRequest req) {
        return ResponseEntity.ok(shopService.exchange(req, principal.teacherId()));
    }

    // 获取兑换记录
    @GetMapping("/records")
    public ResponseEntity<List<ExchangeRecordResponse>> getRecords(
            @AuthenticationPrincipal AuthenticatedTeacher principal) {
        return ResponseEntity.ok(shopService.getRecords(principal.teacherId()));
    }

    // 赠送道具
    @PostMapping("/gift")
    public ResponseEntity<ExchangeRecordResponse> gift(
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @Valid @RequestBody GiftRequest req) {
        return ResponseEntity.ok(shopService.giftItem(req, principal.teacherId()));
    }
}