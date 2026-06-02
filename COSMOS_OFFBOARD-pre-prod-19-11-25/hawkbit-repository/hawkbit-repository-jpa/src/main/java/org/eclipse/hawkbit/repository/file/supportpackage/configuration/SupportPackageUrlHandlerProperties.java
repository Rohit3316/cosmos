package org.eclipse.hawkbit.repository.file.supportpackage.configuration;

import lombok.Data;
import lombok.Getter;


/**
 * Properties class for handling support package URL configurations.
 */
@Getter
public class SupportPackageUrlHandlerProperties {

    private final Esp esp = new Esp();
    private final Rsp rsp = new Rsp();
     private Cdn cdn = new Cdn();

    /**
     * Properties class for handling ESP protocol configuration.
     */
    @Getter
    public static class Esp {

        private final S3 s3 = new S3();
        private final Cdn cdn = new Cdn();

        /**
         * ESP-specific S3 configuration class.
         */
        @Getter
        public static class S3 {

            private final  String directory = "{tenant}/{type}/{SHA256}/";


            @Getter
            public enum Type {
                ESP("esp");

                private final String fileType;

                Type(final String type) {
                    this.fileType = type;
                }
            }

        }

        @Getter
        public static class Cdn {

            private final String directory = "/{tenant}/{type}/{SHA256}";

        }
    }

    /**
     * Properties class for handling RSP protocol configuration.
     */
    @Getter
    public static class Rsp {
        private final S3 s3 = new S3();
        private final Cdn cdn = new Cdn();

        /**
         * RSP-specific S3 configuration class.
         */
        @Getter
        public static class S3 {

            private final String directory = "{tenant}/{type}/{SHA256}/";

            @Getter
            public enum Type {
                RSP("rsp");

                private final String fileType;

                Type(final String type) {
                    this.fileType = type;
                }
            }

        }

        @Getter
        public static class Cdn {

            private final String directory = "/{tenant}/{type}/{SHA256}";

        }
    }

    @Data
    public static class Cdn {

        private String host;

        private String rootDirectory;
        /**
         * Artifacts cdn directory where files are uploaded.
         */
        private String directory = "/{tenant}/{type}/{SHA256}";

    }
}