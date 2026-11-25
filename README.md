# Proyecto-Final-desarrollo-de-software
#  Integrantes:
#  Jose David Hernández
#  Santiago Rojas G
# 
# Comandos Docker:
docker build -t cards4play-api .

docker run -d -p 4567:4567 -v cards4play_data:/app/data -e TASA_DEFECTO=4200.00 --name cards4play-app cards4play-api

docker logs cards4play-app
## API REST


