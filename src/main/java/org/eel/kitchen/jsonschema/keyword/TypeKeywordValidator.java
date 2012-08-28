/*
 * Copyright (c) 2012, Francis Galiegue <fgaliegue@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.eel.kitchen.jsonschema.keyword;

import com.fasterxml.jackson.databind.JsonNode;
import org.eel.kitchen.jsonschema.main.SchemaContainer;
import org.eel.kitchen.jsonschema.main.ValidationContext;
import org.eel.kitchen.jsonschema.main.ValidationMessage;
import org.eel.kitchen.jsonschema.main.ValidationReport;
import org.eel.kitchen.jsonschema.util.NodeType;
import org.eel.kitchen.jsonschema.validator.JsonValidator;
import org.eel.kitchen.jsonschema.validator.JsonValidatorCache;
import org.eel.kitchen.jsonschema.validator.SchemaNode;

/**
 * Validator for the {@code type} keyword
 *
 * <p>This keyword and its counterpart ({@code disallowed}) are two of the
 * most complex keywords.</p>
 */
public final class TypeKeywordValidator
    extends AbstractTypeKeywordValidator
{
    public TypeKeywordValidator(final JsonNode schema)
    {
        super("type", schema);
    }

    @Override
    public void validate(final ValidationContext context,
        final ValidationReport report, final JsonNode instance)
    {
        final NodeType type = NodeType.getNodeType(instance);
        if (typeSet.contains(type))
            return;

        final ValidationReport schemaReport = report.copy();

        if (!schemas.isEmpty()) {
            trySchemas(context, schemaReport, instance);
            if (schemaReport.isSuccess())
                return;
        }

        final ValidationMessage.Builder msg = newMsg().addInfo("found", type)
            .addInfo("allowed", typeSet)
            .setMessage("instance does not match any allowed primitive type");

        report.addMessage(msg.build());
        report.mergeWith(schemaReport);
    }

    private void trySchemas(final ValidationContext context,
        final ValidationReport schemaReport, final JsonNode instance)
    {
        final SchemaContainer orig = context.getContainer();
        final JsonValidatorCache cache = context.getValidatorCache();
        final ValidationReport report = schemaReport.copy();

        ValidationReport subReport;
        JsonValidator validator;
        SchemaNode schemaNode;

        for (final JsonNode schema: schemas) {
            subReport = report.copy();
            schemaNode = new SchemaNode(orig, schema);
            validator = cache.getValidator(schemaNode);
            validator.validate(context, subReport, instance);
            context.setContainer(orig);
            if (subReport.isSuccess())
                return;
            report.mergeWith(subReport);
        }

        schemaReport.mergeWith(report);
    }
}
