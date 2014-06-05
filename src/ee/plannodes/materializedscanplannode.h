/* This file is part of VoltDB.
 * Copyright (C) 2008-2014 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

#ifndef MATERIALIZEDSCANPLANNODE_H
#define MATERIALIZEDSCANPLANNODE_H

#include "abstractscannode.h"

namespace voltdb {

class AbstractExpression;

/**
 * Used for SQL-IN that are accelerated with indexes.
 * See MaterializedScanExecutor for more/eventual use.
 */
class MaterializedScanPlanNode : public AbstractPlanNode {
public:
    MaterializedScanPlanNode() { }
    ~MaterializedScanPlanNode() { }

    virtual PlanNodeType getPlanNodeType() const;

    AbstractExpression* getTableRowsExpression() const { return m_tableRowsExpression; }

    SortDirectionType getSortDirection() const { return m_sortDirection; }

    std::string debugInfo(const std::string &spacer) const;

protected:
    virtual void loadFromJSONObject(PlannerDomValue obj);

private:
    // It doesn't matter what kind of expression this is,
    // so long as eval() returns an NValue array as opposed
    // to the usual scalar NValues.
    OwnedExpression m_tableRowsExpression;
    SortDirectionType m_sortDirection;
};

}

#endif // MATERIALIZEDSCANPLANNODE_H
