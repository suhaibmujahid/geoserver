/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.community.mbstyle.web;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geotools.mbstyle.MapBoxStyle;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Version;
import org.json.simple.parser.ParseException;
import org.xml.sax.EntityResolver;

/**
 * Style handler for MBStyle
 */
public class MBStyleHandler extends StyleHandler {

    public static final String FORMAT = "json";

    public static final String MIME_TYPE = "application/vnd.geoserver.mbstyle+json";

    private SLDHandler sldHandler;

    protected MBStyleHandler(SLDHandler sldHandler) {
        super("MBStyle", FORMAT);
        this.sldHandler = sldHandler;
    }

    @Override
    public StyledLayerDescriptor parse(Object input, Version version,
            ResourceLocator resourceLocator, EntityResolver entityResolver) throws IOException {
        // see if we can use the style cache, some conversions are expensive.
        if (input instanceof File) {
            // convert to resource, to avoid code duplication
            File jsonFile = (File) input;
            input = new FileSystemResourceStore(jsonFile.getParentFile()).get(jsonFile.getName());
        }

        if (input instanceof Resource) {
            Resource jsonResource = (Resource) input;
            Resource sldResource = jsonResource.parent()
                    .get(FilenameUtils.getBaseName(jsonResource.name()) + ".sld");
            if (sldResource.getType() != Resource.Type.UNDEFINED
                    && sldResource.lastmodified() > jsonResource.lastmodified()) {
                // if sld resource exists, use it
                return sldHandler.parse(sldResource, SLDHandler.VERSION_10, resourceLocator,
                        entityResolver);
            } else {
                // otherwise convert and write the cache
                try (Reader reader = toReader(input)) {
                    StyledLayerDescriptor sld = convertToSLD(reader);
                    try (OutputStream fos = sldResource.out()) {
                        sldHandler.encode(sld, SLDHandler.VERSION_10, true, fos);
                    }
                    return sldHandler.parse(sldResource, SLDHandler.VERSION_10, resourceLocator,
                            entityResolver);
                } catch (ParseException e) {
                    throw new IOException(e);
                }
            }
        }

        // in this case, just do a plain on the fly conversion
        try (Reader reader = toReader(input)) {
            return convertToSLD(toReader(input));
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    private StyledLayerDescriptor convertToSLD(Reader reader) throws IOException, ParseException {
        return MapBoxStyle.parse(reader);
    }

    @Override
    public void encode(StyledLayerDescriptor sld, Version version, boolean pretty,
            OutputStream output) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exception> validate(Object input, Version version, EntityResolver entityResolver)
            throws IOException {        
        return MapBoxStyle.validate(toReader(input));
    }

    @Override
    public String mimeType(Version version) {
        return MIME_TYPE;
    }

}
