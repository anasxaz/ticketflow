# ---- Etape 1 : compiler l'appli Angular ----
FROM node:26-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci                                   # installe exactement les versions du package-lock
COPY . .
RUN npx ng build --configuration production  # -> dist/frontend/browser

# ---- Etape 2 : servir les fichiers statiques avec nginx ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
