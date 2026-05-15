package com.classpet.dto;

/**
 * 进化选项DTO - 表示一个可用的进化目标
 */
public class EvolutionOptionDto {
    private Integer toPokedexId;
    private String toName;
    private String toImage;
    private String toTypes;
    private String condition;      // 进化条件描述
    private String toFormName;     // Mega/ Gigantamax形态名称（如果有）
    private Integer toStage;       // 目标阶段：1=一阶，2=二阶
    
    public EvolutionOptionDto() {}
    
    public EvolutionOptionDto(Integer toPokedexId, String toName, String toImage, 
                              String toTypes, String condition, String toFormName, Integer toStage) {
        this.toPokedexId = toPokedexId;
        this.toName = toName;
        this.toImage = toImage;
        this.toTypes = toTypes;
        this.condition = condition;
        this.toFormName = toFormName;
        this.toStage = toStage;
    }
    
    // Getters and Setters
    public Integer getToPokedexId() { return toPokedexId; }
    public void setToPokedexId(Integer toPokedexId) { this.toPokedexId = toPokedexId; }
    public String getToName() { return toName; }
    public void setToName(String toName) { this.toName = toName; }
    public String getToImage() { return toImage; }
    public void setToImage(String toImage) { this.toImage = toImage; }
    public String getToTypes() { return toTypes; }
    public void setToTypes(String toTypes) { this.toTypes = toTypes; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getToFormName() { return toFormName; }
    public void setToFormName(String toFormName) { this.toFormName = toFormName; }
    public Integer getToStage() { return toStage; }
    public void setToStage(Integer toStage) { this.toStage = toStage; }
}
