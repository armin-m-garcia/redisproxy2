package io.segment;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class URIPropertyConverter implements Converter<String, URI> {
    @Override
    public URI convert(String source) {
        if(source==null){
            return null;
        }
        
        return URI.create(source);
    }
}