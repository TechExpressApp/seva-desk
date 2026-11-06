package com.sevadesk.donors;

import com.sevadesk.platform.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** What a donation is for: General, Annadanam, Gau Seva, Building Fund… Named per temple. */
@Entity
@Table(name = "fund")
public class Fund extends TenantScopedEntity {

    private String name;
    @Column(name = "tax_deductible") private boolean taxDeductible = true;
    private boolean active = true;

    protected Fund() {}

    public Fund(String name, boolean taxDeductible) {
        this.name = name;
        this.taxDeductible = taxDeductible;
    }

    public String getName() { return name; }
    public boolean isTaxDeductible() { return taxDeductible; }
    public boolean isActive() { return active; }
}
