## 1. Setup
- Chạy với quyền cao nhất
    ```
    sudo su
    ```
- Update package manager
    ```
    apt update
    apt-get update
    ```
- Tạo thư mục '/var/www/html'
    ```
    mkdir -p /var/www/html
    ```
- Clone source code project master 'https://github.com/dttrung257/agent_simulation_api'
    ```
    cd /var/www/html
    git clone https://github.com/dttrung257/agent_simulation_worker
    cd /var/www/html/agent_simulation_worker
    git checkout develop
    ```
- Install GAMA
    ```
    cd /var/www/html/agent_simulation_worker/gama
    apt update
    apt install wget unzip
    wget https://github.com/gama-platform/gama/releases/download/1.9.3/GAMA_1.9.3_Linux.zip
    unzip GAMA_1.9.3_Linux.zip -d gama-platform
    rm −f GAMA_1.9.3_Linux.zip
    ```
- Install Project
    ```
    cd /var/www/html/agent_simulation_worker/projects
    git clone <project_link> (Example: https://github.com/dhdang2003/pig-farm.git)
    cd /var/www/html/agent_simulation_worker/projects/<project_name>
    git checkout <branch>
    ```
- Install Docker
    ```
    for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do sudo apt-get remove $pkg; done
    ```
    
    ```
    # Add Docker's official GPG key:
    sudo apt-get update
    sudo apt-get install ca-certificates curl
    sudo install -m 0755 -d /etc/apt/keyrings
    sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
    sudo chmod a+r /etc/apt/keyrings/docker.asc

    # Add the repository to Apt sources:
    echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
    sudo apt-get update
    ```

    ```
    sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    ```

    ```
    sudo groupadd docker
    sudo usermod -aG docker $USER
    newgrp docker
    ```

    ```
    docker --version
    ```
- Thêm file .env
    ```
    SERVER_PORT=8080

    SPRING_PROFILES_ACTIVE=local
    SPRING_APPLICATION_NAME=ags-api
    SPRING_THREADS_VIRTUAL_ENABLED=true

    MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE=*
    MANAGEMENT_ENDPOINT_METRICS_ENABLED=true

    APP_TIMEZONE=UTC+7

    LOGGING_LEVEL_ROOT=INFO
    LOGGING_LEVEL_REDIS_CLIENTS_JEDIS=DEBUG
    LOGGING_FILE_PATH=storage/logs

    SPRING_DATASOURCE_URL=jdbc:mysql://18.143.132.221:3306/ags
    SPRING_DATASOURCE_USERNAME=agsuser
    SPRING_DATASOURCE_PASSWORD=mysql123456
    SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=30

    SPRING_JPA_HIBERNATE_DDL_AUTO=validate
    SPRING_JPA_SHOW_SQL=false
    SPRING_JPA_PROPERTIES_HIBERNATE_JDBC_BATCH_SIZE=1000
    SPRING_JPA_PROPERTIES_HIBERNATE_ORDER_INSERTS=true
    SPRING_JPA_PROPERTIES_HIBERNATE_ORDER_UPDATES=true
    SPRING_JPA_PROPERTIES_FORMAT_SQL=true

    SPRING_FLYWAY_ENABLED=true
    SPRING_FLYWAY_BASELINE_ON_MIGRATE=true
    SPRING_FLYWAY_LOCATIONS=classpath:db/migration
    SPRING_FLYWAY_URL=jdbc:mysql://db:3306/ags

    SPRING_DATA_REDIS_HOST=18.143.132.221
    SPRING_DATA_REDIS_PORT=6379
    SPRING_DATA_REDIS_PASSWORD=redis123456
    SPRING_DATA_REDIS_DATABASE=0

    WEBFLUX_BODY_MAX_SIZE_MB=10

    #AWS_ACCESS_KEY=
    #AWS_SECRET_KEY=
    #AWS_REGION=
    #AWS_S3_BUCKET_NAME=

    JWT_SECRET=dt}kkS%py8,Xh30!GLWr@FE@7CC,x@#d-{vni9fvjumkgS.6ex056?7RJgH?a=}K
    JWT_TTL=7776000000
    JWT_REFRESH_TTL=2419200000

    GAMA_PATH_SHELL=/opt/gama-platform/headless/gama-headless.sh
    GAMA_PATH_OUTPUT=/app/storage/outputs
    GAMA_PATH_XML=/app/storage/xmls
    GAMA_PATH_PROJECT=/app/projects
    GAMA_CONFIG_FRAME_RATE=45

    CLUSTER_CONFIG_PATH=src/main/resources/cluster-config.yml
    ```

- Thêm file cluster-config.yml
    ```
    vi /var/www/html/agent_simulation_worker/src/main/resources/cluster-config.yml
    ```
    Với nội dung:
    ```
    cluster:
    node_id: 2
    node_role: 2
    host: <ip>
    port: 8080
    node_name: worker-[number]
    ```

- Deploy
    ```
    cd /var/www/html/agent_simulation_worker
    git fetch origin develop; git reset --hard origin/develop
    docker compose up --build -d; docker logs -f ags_dev_worker
    ```
- Rebuild
    ```
    docker compose stop api; docker compose up api --build -d
    docker logs -f ags_dev_worker
    ```
- Clean after deploy
    ```
    docker system prune -a --volumes
    ```
- Cách đổi IP config cho cluster
    - Sửa IP trong file cluster-config
    ```
    vi /var/www/html/agent_simulation_worker/src/main/resources/cluster-config.yml
    ```
    - Sau đó restart lại container api
    ```
    docker compose restart api; docker logs -f ags_dev_worker
    ```
- Truy cập database DEV từ console (Đã ssh)
    ```
    docker exec -it ags_dev_db mysql -u agsuser ags -pmysql123456
    ```
- Restart API container
    ```
    docker compose restart api; docker logs -f ags_dev_api
    ```
- Tạo lại API container
    ```
    docker compose down api; docker compose up api -d; docker logs -f ags_dev_api
    ```
