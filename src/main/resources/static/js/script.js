console.log("this is script file")

const toggleSidebar=()=>{

    if ($(".sidebar").is(":visible"))
    {
        // if condition true then need to close it

        $(".sidebar").css("display", "none");
        $(".content").css("margin-left", "0%");


    }
    else {
        // if false then need to show it
        $(".sidebar").css("display", "block");
        $(".content").css("margin-left", "20%");
    }
};

const search=()=>{
    // console.log("Searching...");

    let query=$("#search-input").val();
    console.log(query);

    if (query == "")
    {
        $(".search-result").hide();

    }
    else {
        //search
        console.log(query);

        //sending request to server

        let url=`http://localhost:8080/search/${query}`;

        fetch(url)
            .then((response) => {
            return response.json();
        })
            .then((data) => {
                //data...
                // console.log(data);
                let text= `<div class='list-group' >`

                 data.forEach(contact => {
                    text+=`<a href="/user/${contact.cId}/contact" 
                                class="list-group-item list-group-item-action"> ${contact.name} 
                            </a>`
                 });

                text+=`</div>`;

                $(".search-result").html(text);
                $(".search-result").show();
        });
    }

};

// first request: to server to create order

const paymentStart = () =>{
    console.log("Payment started..");
    let amount=$("#payment_field").val();
    console.log(amount);
    if (amount=="" || amount==null)
    {
        // alert("amount is required!!");
        swal("Failed!!", "amount is required..!", "error");
        return;
    }

    //We wil use ajax to send request on server to create order - jquery

    $.ajax(
        {
            url: "/user/create_order",
            data: JSON.stringify({amount:amount, info:"order_request"}),
            contentType: "application/json",
            type: "POST",
            dataType:"json",
            success:function (response) {
                // invoked when success
                console.log(response);
                if (response.status == "created")
                {
                    // Open payment form
                    let options={
                        key:"rzp_test_v3x9FAyRos2UEk",
                        amount:response.amount,
                        currency:"INR",
                        name:"SyncSphere Contacts Master",
                        description:"Donation",
                        // image:"http://localhost:8080/img/scm_logo2.jpg",
                        image:"/img/scm_logo2.jpg",
                        order_id:response.id,
                        handler:function (response) {
                            console.log(response.razorpay_payment_id);
                            console.log(response.razorpay_order_id);
                            console.log(response.razorpay_signature);
                            console.log("Payment successful !!");
                            // alert("Congrats!! Payment successful. ");

                            updatePaymentOnServer(response.razorpay_payment_id,
                                response.razorpay_order_id, "paid"
                            );

                            swal("Congrats!", "Payment successful..!", "success");
                        },
                        prefill: { //We recommend using the prefill parameter to auto-fill customer's contact information especially their phone number
                            name: "", //your customer's name
                            email: "",
                            // contact: "9992501364",
                            contact: "", //Provide the customer's phone number for better conversion rates
                        },
                        notes: {
                            address: "SyncSphere Contacts Master"
                        },
                        theme: {
                            "color": "#3399cc"
                        }
                    };

                   let rzp = new Razorpay(options);
                    rzp.on('payment.failed', function (response){
                        console.log(response.error.code);
                        console.log(response.error.description);
                        console.log(response.error.source);
                        console.log(response.error.step);
                        console.log(response.error.reason);
                        console.log(response.error.metadata.order_id);
                        console.log(response.error.metadata.payment_id);
                        alert("Oops payment failed !!");
                        swal("Oops!", "Payment failed..!", "error");
                    });

                   rzp.open();
                }
            },

            error:function (error) {
                // invoked when error
                console.log(error);
                alert("Something went wrong!!");
            },

        }
    );

    // Clear the input field after processing
    document.getElementById("payment_field").value = "";

};

//
function updatePaymentOnServer(payment_id, order_id, status)
{
    $.ajax(
        {
            url: "/user/update_order",
            data: JSON.stringify({payment_id: payment_id, order_id: order_id, status:status }),
            contentType: "application/json",
            type: "POST",
            dataType: "json",
            success: function (response) {
                swal("Congrats!", "Payment successful..!", "success");
            },
            error:function (error) {
                swal("Failed!!",
                    "your payment is successful, but we didn't get on server. We will confirm you asap ",
                    "error");
            },
        });

}