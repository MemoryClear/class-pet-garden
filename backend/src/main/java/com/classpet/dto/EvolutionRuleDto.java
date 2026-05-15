package com.classpet.dto;

public class EvolutionRuleDto {
    private Integer fromPokedexId;
    private String fromName;
    private Integer toPokedexId;
    private String toName;
    private String toImage;
    private String toTypes;
    private Integer stage;
    private Integer levelRequired;
    private Integer foodRequired;
    
    // Getters and Setters
    public Integer getFromPokedexId() { return fromPokedexId; }
    public void setFromPokedexId(Integer fromPokedexId) { this.fromPokedexId = fromPokedexId; }
    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
    public Integer getToPokedexId() { return toPokedexId; }
    public void setToPokedexId(Integer toPokedexId) { this.toPokedexId = toPokedexId; }
    public String getToName() { return toName; }
    public void setToName(String toName) { this.toName = toName; }
    public String getToImage() { return toImage; }
    public void setToImage(String toImage) { this.toImage = toImage; }
    public String getToTypes() { return toTypes; }
    public void setToTypes(String toTypes) { this.toTypes = toTypes; }
    public Integer getStage() { return stage; }
    public void setStage(Integer stage) { this.stage = stage; }
    public Integer getLevelRequired() { return levelRequired; }
    public void setLevelRequired(Integer levelRequired) { this.levelRequired = levelRequired; }
    public Integer getFoodRequired() { return foodRequired; }
    public void setFoodRequired(Integer foodRequired) { this.foodRequired = foodRequired; }
}