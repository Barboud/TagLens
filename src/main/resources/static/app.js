const API = location.hostname === "localhost" ? "http://localhost:8080" : location.origin;

const gallery = document.getElementById("gallery");

async function loadImages(search = "") {

    let url = API + "/images";

    if (search !== "")
        url += "?search=" + encodeURIComponent(search);

    const response = await fetch(url);

    const images = await response.json();

    gallery.innerHTML = "";

    images.forEach(createCard);

}

function createCard(image) {

    const card = document.createElement("div");

    card.className = "card";

    card.innerHTML = `

        <img src="${image.url}">

        <div class="content">

            <h3>${image.filename}</h3>

            <div class="tags">

                ${
        image.tags.tags
            .map(tag=>`<span class="tag">${tag}</span>`)
            .join("")
    }

            </div>

        </div>

    `;

    gallery.appendChild(card);

}

document
    .getElementById("searchBtn")
    .onclick = ()=>{

    loadImages(
        document.getElementById("searchInput").value
    );

};

document
    .getElementById("homeBtn")
    .onclick = ()=>loadImages();


document
    .getElementById("uploadBtn")
    .onclick = async ()=>{

    const file =
        document.getElementById("imageInput").files[0];

    if(!file)
        return;

    const form = new FormData();

    form.append("file",file);

    const response = await fetch(API+"/images/upload",{

        method:"POST",

        credentials:"include",

        body:form

    });

    if(response.ok){

        alert("Uploaded!");

        loadImages();

    }

    else{

        alert("Upload failed");

    }

};


async function login(email,password){

    await fetch(API+"/auth/login",{

        method:"POST",

        credentials:"include",

        headers:{

            "Content-Type":"application/json"

        },

        body:JSON.stringify({

            email,

            password

        })

    });

}

async function register(email,password){

    await fetch(API+"/auth/register",{

        method:"POST",

        headers:{

            "Content-Type":"application/json"

        },

        body:JSON.stringify({

            email,

            password

        })

    });

}

async function logout(){

    await fetch(API+"/auth/logout",{

        method:"POST",

        credentials:"include"

    });

}

loadImages();