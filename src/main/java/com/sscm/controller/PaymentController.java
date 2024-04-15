package com.sscm.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.sscm.dao.PaymentOrderRepository;
import com.sscm.dao.UserRepository;
import com.sscm.entities.PaymentOrder;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class PaymentController {

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private PaymentOrderRepository paymentOrderRepos;

    // Creating order for payment
    @PostMapping("/create_order")
    @ResponseBody
    public String createOrder(@RequestBody Map<String, Object> data, Principal principal) throws Exception {
//        System.out.println("order function executed.");
//        System.out.println(data);

        double amt = Double.parseDouble(data.get("amount").toString());
//        System.out.println(amt);

        var client = new RazorpayClient("rzp_test_v3x9FAyRos2Ucv","8v6n0c5EMZobNeKSD2wFU9fK");

        JSONObject obj = new JSONObject();
        obj.put("amount", amt*100);
        obj.put("currency","INR");
        obj.put("receipt","txn_2411023");

        //Creating new order
        Order order = client.orders.create(obj);
//        System.out.println(order);

        //save the order info in database:
        PaymentOrder myOrder = new PaymentOrder();

        double amnt = Double.parseDouble(order.get("amount").toString());
        myOrder.setAmount((amnt/100)+""); // convert into integer rupees
//        System.out.println("amount to save in DB : " +(amnt/100));

        myOrder.setOrderId(order.get("id"));
        myOrder.setPaymentId(null);
        myOrder.setStatus("created");
        myOrder.setUser(userRepos.getUserByUserName(principal.getName()));
        myOrder.setReceipt(order.get("receipt"));

        paymentOrderRepos.save(myOrder);

        return order.toString();
    }

    //handler for update order
    @PostMapping("/update_order")
    public ResponseEntity<?> updateOrder(@RequestBody Map<String, Object> data)
    {
        PaymentOrder myOrder = paymentOrderRepos.findByOrderId(data.get("order_id").toString());
        myOrder.setPaymentId(data.get("payment_id").toString());
        myOrder.setStatus(data.get("status").toString());

        paymentOrderRepos.save(myOrder);

//        System.out.println(data);
        return ResponseEntity.ok(Map.of("msg", "updated"));
    }

}
