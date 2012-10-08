maven-document-to-pdf-plugin
============================

Creates PDF files from Office documents.

Sample configuration:
    <build>
        <plugins>
            <plugin>
              <groupId>com.squins.maven.plugins</groupId>
                <artifactId>document-to-pdf-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>export</goal>
                        </goals>
                        <phase>generate-resources</phase>
                    </execution>
                </executions>
                <configuration>
                    <documentDirectory>${project.basedir}/src/main/documents</documentDirectory>
                    <outputDirectory>${project.build.outputDirectory}</outputDirectory>
                </configuration>
            </plugin>
        </plugins>
    </build>
