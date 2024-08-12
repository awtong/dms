package awt.dms;

import awt.dms.config.MongoDbContainerConfig;
import awt.dms.config.RabbitContainerConfig;
import org.springframework.boot.SpringApplication;

public class LocalDmsApplication {

  public static void main(String[] args) {
    SpringApplication.from(DmsApplication::main)
        .with(MongoDbContainerConfig.class)
        .with(RabbitContainerConfig.class)
        .run(args);
  }
}
