package com.classpet.dto;

/**
 * 进化规则条目 - 对应 evolution_rules.json 中的单个规则
 */
public class EvolutionRuleEntry {
    private Integer toPokedexId;
    private String toName;
    private String condition;
    private String toFormName;
    private Integer fromStage;
    private Integer toStage;
    private Boolean implemented;
    
    public EvolutionRuleEntry() {}
    
    public Integer getToPokedexId() { return toPokedexId; }
    public void setToPokedexId(Integer toPokedexId) { this.toPokedexId = toPokedexId; }
    public String getToName() { return toName; }
    public void setToName(String toName) { this.toName = toName; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getToFormName() { return toFormName; }
    public void setToFormName(String toFormName) { this.toFormName = toFormName; }
    public Integer getFromStage() { return fromStage; }
    public void setFromStage(Integer fromStage) { this.fromStage = fromStage; }
    public Integer getToStage() { return toStage; }
    public Boolean getImplemented() { return implemented; }
    public void setToStage(Integer toStage) { this.toStage = toStage; }
    public void setImplemented(Boolean implemented) { this.implemented = implemented; }
}
