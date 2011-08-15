/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.synchronization.work;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.configuration.metadata.MetadataTools;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class CollectionChangeWorkUnit extends AbstractAuditWorkUnit implements AuditWorkUnit {
    private final Object entity;
	private final String collectionPropertyName;
	private final Map<String, Object> data = new HashMap<String, Object>();

    public CollectionChangeWorkUnit(SessionImplementor session, String entityName, String collectionPropertyName,
									AuditConfiguration verCfg, Serializable id, Object entity) {
        super(session, entityName, verCfg, id, RevisionType.MOD);
        this.entity = entity;
		this.collectionPropertyName = collectionPropertyName;
		assert collectionPropertyName != null;
    }

    public boolean containsWork() {
        return true;
    }

    public Map<String, Object> generateData(Object revisionData) {
        fillDataWithId(data, revisionData);
		verCfg.getEntCfg().get(getEntityName()).getPropertyMapper()
				.mapToMapFromEntity(sessionImplementor, data, entity, null);
		verCfg.getEntCfg().get(getEntityName()).getPropertyMapper()
				.mapModifiedFlagsToMapFromEntity(sessionImplementor, data, entity, entity);
		verCfg.getEntCfg().get(getEntityName()).getPropertyMapper()
				.mapModifiedFlagsToMapForCollectionChange(collectionPropertyName, data);
        return data;
    }

	public void addCollectionModifiedData(Map<String, Object> data) {
		String modifiedFlagForCollection = getModifiedFlagPropertyNameForCollection();
		if(data.containsKey(modifiedFlagForCollection)) {
			data.put(modifiedFlagForCollection, true);
		}
	}

	private String getModifiedFlagPropertyNameForCollection() {
		int dotIdx = collectionPropertyName.indexOf('.');
		if (dotIdx != -1) { // in component
			return MetadataTools.getModifiedFlagPropertyName(collectionPropertyName.substring(0, dotIdx));
		}
		return MetadataTools.getModifiedFlagPropertyName(collectionPropertyName);
	}

	public AuditWorkUnit merge(AddWorkUnit second) {
        return second;
    }

    public AuditWorkUnit merge(ModWorkUnit second) {
        return second;
    }

    public AuditWorkUnit merge(DelWorkUnit second) {
        return second;
    }

    public AuditWorkUnit merge(CollectionChangeWorkUnit second) {
        return this;
    }

    public AuditWorkUnit merge(FakeBidirectionalRelationWorkUnit second) {
        return second;
    }

    public AuditWorkUnit dispatch(WorkUnitMergeVisitor first) {
        return first.merge(this);
    }
}
